#lang racket

(define (fact n)
  (if (= n 0)
      1
      (* n (fact (- n 1)))
      )
  )

(define (length l)
  (if (empty? l)
      0
      (+ 1 (length (rest l)))
      )
  )