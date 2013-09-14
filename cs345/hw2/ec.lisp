#lang racket
; Homework 2
; Name: Shun Zhang
; UT EID: sz4554
;
; Extra

(letrec ((foldr-rec (lambda (op l) (if (null? (cdr l)) (car l) (op (car l) (foldr-rec op (cdr l))))))
		 (foldr (lambda (op v l) (if (null? l) v (op v (foldr-rec op l)))))
		 (map (lambda (func l)
		 			  (let ((op (lambda (x y) (if (list? y) (cons x (cons (func (car y)) (cdr y))) (cons x (cons (func y) '()) )))))
					  	   (if (null? l) null (cdr (foldr op '() l)))))))

		(map (lambda (x) (+ x 5)) '(1 2 3 4 5 6)))
