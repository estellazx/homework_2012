#lang racket
; Homework 2
; Name: Shun Zhang
; UT EID: sz4554
;
; Q2

(letrec ((pd (lambda (x) (if (= x 0) 0 (- x 1))))
		 (dm (lambda (x y) (if (= y 0) x (pd (dm x (- y 1))))))
		 (abs (lambda (x y) (+ (dm x y) (dm y x))))
		 (sg (lambda (x) (if (= x 0) 0 1)))
		 (eq (lambda (x y) (sg (abs x y)))))
	 	(cons (eq 11 11) (cons (eq 11 12) (cons (eq 12 11) '() ))))
