#lang plai
 
;; (calc (parse '5) )
;; (calc (parse '{+ 5 5}) )
;; (calc (parse '{with {x {+ 5 5}} {+ x x}}) )
;; (calc (parse '{with {x 5} {+ x x}}) )
;; (calc (parse '{with {x {+ 5 5}} {with {y {- x 3}} {+ y y}}}) )
;; (calc (parse '{with {x 5} {with {y {- x 3}} {+ y y}}}) )
;; (calc (parse '{with {x 5} {+ x {with {x 3} 10}}}) )
;; (calc (parse '{with {x 5} {+ x {with {x 3} x}}}) )
;; (calc (parse '{with {x 5} {+ x {with {y 3} x}}}) )
;; (calc (parse '{with {x 5} {with {y x} y}}) )
;; (calc (parse '{with {x 5} {with {x x} x}}) )

(define-type WAE
  [num (n number?)]
  [add (lhs WAE?) (rhs WAE?)]
  [sub (lhs WAE?) (rhs WAE?)]
  [mul (lhs WAE?) (rhs WAE?)]
  [div (lhs WAE?) (rhs WAE?)]
  [with (name symbol?) (named-expr WAE?) (body WAE?)]
  [id (name symbol?)])

(define parse
  (lambda (sexp)
    (cond
      [(number? sexp) (num sexp)]
      [(symbol? sexp) (id sexp)]
      [(list? sexp)
       (case (first sexp)
         [(+)(add (parse (second sexp))
                  (parse (third sexp)))]
         [(-) (sub (parse (second sexp))
                   (parse (third sexp)))]
         [(*) (mul (parse (second sexp))
                   (parse (third sexp)))]
         [(/) (div (parse (second sexp))
                   (parse (third sexp)))]
         [(with) (with (first (second sexp))
                       (parse (second (second sexp)))
                       (parse (third sexp)))]
         )])))

;; subst : WAE symbol WAE!WAE
(define (subst expr sub-id val)
  (type-case WAE expr
			 [num (n) expr]
			 [add (l r) (add (subst l sub-id val)
											 (subst r sub-id val))]
			 [sub (l r) (sub (subst l sub-id val)
											 (subst r sub-id val))]
	     [mul (l r) (mul (subst l sub-id val)
			     (subst r sub-id val))]
	     [div (l r) (div (subst l sub-id val)
			     (subst r sub-id val))]
             [with (bound-id named-expr bound-body)
                   (if (symbol=? bound-id sub-id)
                       (with bound-id
                             (subst named-expr sub-id val)
                             bound-body)
                       (with bound-id
                             (subst named-expr sub-id val)
                             (subst bound-body sub-id val)))]
             [id (v) (if (symbol=? v sub-id) val expr)]))

;; calc : WAE!number
(define (calc expr)
  (type-case WAE expr
             [num (n) n]
             [add (l r) (+ (calc l) (calc r))]
             [sub (l r) (- (calc l) (calc r))]
             [mul (l r) (* (calc l) (calc r))]
             [div (l r) (/ (calc l) (calc r))]
             [with (bound-id named-expr bound-body)
                   (calc (subst bound-body
                                bound-id
                                (num (calc named-expr))))]
             [id (v) (error 'calc "free identifier")]))
