#lang racket
; Homework 2
; Name: Shun Zhang
; UT EID: sz4554
;
; Q4

(letrec ((foldl-rec (lambda (op sum l) (if (null? l) sum (foldl-rec op (op sum (car l)) (cdr l)))))
		 (foldl (lambda (op v l) (if (null? l) v (op v (foldl-rec op (car l) (cdr l)))))))

		(foldl + 0 '(1 2 3 4 5 6)))
