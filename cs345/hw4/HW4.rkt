; Name: Shun Zhang
; EID: sz4554
#lang plai
 
; Example from page 46
; {with  {x 3}  {with  {f {fun {y}    {+ x y}}}  {with  {x 5}  {f 4}}}}
; (let  ((x 3)) (let  ((f (lambda (y) (+ x y)))) (let  ((x 5)) (funcall f 4)))) - works in jdblisp

(define-type FAE
  (num (n number?))
  (id (name symbol?))
  (add (lt FAE?) (rt FAE?))
  (sub (lt FAE?) (rt FAE?))
  (mul (lt FAE?) (rt FAE?))
  (div (lt FAE?) (rt FAE?))
  (fun (arg-name symbol?) (body FAE?))
  (app (fun-exp FAE?) (arg-exp FAE?)))

; I've already done parse to save some time.

(define (parse sexp)
  (cond [(number? sexp) (num sexp)]
        [(symbol? sexp) (id sexp)]
        [(list? sexp)
         (case (first sexp)
           ((+)
            (add (parse (second sexp))
                 (parse (third sexp))))
           ((-)
            (sub (parse (second sexp))
                 (parse (third sexp))))
           ((*)
            (mul (parse (second sexp))
                 (parse (third sexp))))
           ((/)
            (div (parse (second sexp))
                 (parse (third sexp))))
           ((with)                                 ; Notice how we parse with!!!!
            (app (fun (first (second sexp))        ; 
                      (parse (third sexp)))        ; Solves exercise 6.3.1 
                 (parse (second (second sexp)))))  ;
           ((fun)                                  
            (fun (first (second sexp))             
                 (parse (third sexp))))            
           (else
            (app (parse (first sexp))            
                 (parse (second sexp)))))]))
; TRY 3: correct interpreter with deferred substitution.

(define-type FAE-value ; what the interp function returns
  (numV (n number?))
  (closureV (arg-name symbol?) (body FAE?) 
            (ds Sub?) ; this is how we remember the substitutions!
            ))

(define (num+ x y) ;; need this because we can't just use Scheme + to add FAE-values
  (numV (+ (numV-n x) (numV-n y))))
(define (num- x y)
  (numV (- (numV-n x) (numV-n y))))
(define (num* x y)
  (numV (* (numV-n x) (numV-n y))))
(define (num/ x y)
  (numV (/ (numV-n x) (numV-n y))))

(define-type Sub
  (mtSub)
  (aSub (id symbol?) 
        (val FAE-value?) 
        (more-subs Sub?)))

(define (lookup name ds)
  (type-case Sub ds
    [mtSub () (error 'lookup "free identifier")]
    [aSub (bound-name bound-value rest-ds)
          (if (symbol=? bound-name name)
              bound-value
              (lookup name rest-ds))]))

;interp : FAE Sub -> FAE-value
(define (interp exp ds)
  (type-case FAE exp
     (num (n)    (numV n))
     (add (l r)  (num+ (interp l ds) (interp r ds)))
     (sub (l r)  (num- (interp l ds) (interp r ds)))
     (mul (l r)  (num* (interp l ds) (interp r ds)))
     (div (l r)  (num/ (interp l ds) (interp r ds)))
     (id  (name) (lookup name ds))
     (fun (arg-name body) (closureV arg-name body ds))
     (app (fun-exp arg-exp)
          (let ((the-fun (interp fun-exp ds))
                (the-arg (interp arg-exp ds)))
            (type-case FAE-value the-fun
              (closureV (arg-name body closure-ds)
                        (interp body (aSub arg-name the-arg
                                           ; ds))) ;-> dynamic scope
                                           ;(mtSub)))) ;-> function can only see its own arguments
                                           closure-ds))) ;-> static scope
              (else (error "You can only apply closures!")))))))
;Examples that trigger the "You can only apply closures!" error:
'{with {f {fun {x} {+ x x}}}
    {with {g 3}
        {+ {f 100} {g 100}}}}
'{1 2}
; - - - - - TESTS - - - - - - - -

;Notice Parse removes "withs" in the abstract syntax
(parse '{with  {x 3}  {with  {f {fun {y}    {+ x y}}}  {with  {x 5}  {f 4}}}})
(test (interp (parse '1) (mtSub)) (numV 1))
(test (interp (parse '{+ 1 2}) (mtSub)) (numV 3))

(test (interp (parse '{+ 5 5}) (mtSub)) (numV 10))
(test (interp (parse '{{fun {x} {+ x x}} 5}) (mtSub)) (numV 10))
(test (interp (parse '{with {f {with {y 5} {fun {x} {+ x y}}}} {f 1}}) (mtSub)) (numV 6))
(test (interp (parse '{with {double {fun {x} {+ x x}}} {double 5}}) (mtSub)) (numV 10))
;(let ((double (lambda (x) (+ x x)))) (funcall double 5)) - this works in jdblisp

; The following example will give (numV 9) for Dynamic Scoping and (numV 7) for Static Scoping. See line 161 to change between these.
(interp (parse '{with {x 3}
                            {with {f {fun {y} {+ x y}}}
                                  {with {x 5}
                                        {f 4}}}}) (mtSub))

; - - - - - HOMEWORK - - - - -
; assume f is f(x) = x
; d/dx expression
(interp (parse '{with {H 0.0001}
											{with {f {fun {x} x }}
														{with {d/dx {fun {x} {/ {- {f {+ x H}} {f x}} H}}}
																{d/dx 10}}}}) (mtSub))

; put everything apart from d/dx application to evironment
(interp (parse '{d/dx 10})
				(aSub 'd/dx (closureV 'x (div (sub (app (id 'f) (add (id 'x) (id 'H))) (app (id 'f) (id 'x))) (id 'H)) (aSub 'f (closureV 'x (id 'x) (mtSub)) (aSub 'H (numV 0.0001) (mtSub)))) (mtSub)))
