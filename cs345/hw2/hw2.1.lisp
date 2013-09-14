#lang racket
; Homework 2
; Name: Shun Zhang
; UT EID: sz4554
;
; Q1

(letrec ((f1 (lambda (x) (+ x 1)))
		 (f2 (lambda (x) x))
		 (f3 (lambda (y z x) z))
		 (f4 (lambda (y z x) (f1 (f3 y z x))))
		 (f5 (lambda (y x)
		     (if (= y 0)
		     	 (f2 x)
				 (f4 (- y 1) (f5 (- y 1) x) x))))
		 (f6 (lambda (y z x) x))
		 (f7 (lambda (y z x) (f5 (f3 y z x) (f6 y z x))))
		 (f8 (lambda (x) 0))
		 (f9 (lambda (y x)
		 	 (if (= y 0)
			 	 (f8 x)
				 (f7 (- y 1) (f9 (- y 1) x) x))))
		 (f10 (lambda (y z x) (f9 (f3 y z x) (f6 y z x))))
		 (f11 (lambda (x) 1))
		 (f12 (lambda (y x)
		 	  (if (= y 0)
			  	  (f11 x)
				  (f10 (- y 1) (f12 (- y 1) x) x)))))

		(f12 10 2))
