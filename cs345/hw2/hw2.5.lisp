#lang racket
; Homework 2
; Name: Shun Zhang
; UT EID: sz4554
;
; Q5

(letrec ((foldr-rec (lambda (op l) (if (null? (cdr l)) (car l) (op (car l) (foldr-rec op (cdr l))))))
		 (foldr (lambda (op v l) (if (null? l) v (op v (foldr-rec op l))))))

		(foldr + 0 '(1 2 3 4 5 6)))
