; Name: Shun Zhang
; EID: sz4554
#lang plai

(let ((H 0.0001))
		 (let	((f (lambda (x) (* x x))))
					(let ((d/dx (lambda (x) (/ (- (f (+ x H)) (f x)) H))))
							 d/dx)))

; (let ((A B)) C) -> ((lambda (A C)) B)
((lambda (H) ((lambda (f) (lambda (x) (/ (- (f (+ x H)) (f x)) H))) (lambda (x) (* x x)))) 0.001)
