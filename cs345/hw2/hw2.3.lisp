#lang racket
; Homework 2
; Name: Shun Zhang
; UT EID: sz4554
;
; Q3

(letrec ((pd (lambda (x) (if (= x 0) 0 (- x 1))))
		 (dm (lambda (x y) (if (= y 0) x (pd (dm x (- y 1))))))
		 (less (lambda (x y) (if (= (dm y x) 0) 1 0))))
	 	(cons (less 11 12) (cons (less 11 11) (cons (less 12 11) '() ))))

