p.
q.
r.
s :- p, q.
t :- s, r.

parent(hank,ben).
parent(hank,denise).
parent(irene,ben).
parent(irene,denise).
parent(alice,carl).
parent(ben,carl).
parent(denise,frank).
parent(denise,gary).
parent(earl,frank).
parent(earl,gary).

grandparent(X,Z) :- parent(X,Y) , parent(Y,Z).

ancestor(X,Y) :- parent(X,Y).
ancestor(X,Y) :- parent(Z,Y) , ancestor(X,Z).

floors([floor(_,5),floor(_,4),floor(_,3),floor(_,2),floor(_,1)]).
building(Floors) :- floors(Floors),
        bmember(floor(baker, B), Floors),
				        bmember(floor(cooper, C), Floors),
								        bmember(floor(fletcher, F), Floors),
												        bmember(floor(miller, M), Floors),
																        bmember(floor(smith, S), Floors).

																				bmember(X, [X | _]).
																				bmember(X, [_ | Y]) :- bmember(X, Y).

																				head( [ X | _ ], X).

																				tail( [ _ | Xs ], Xs).

																				null( [] ).

																				lastelem( [ X ], X).
																				lastelem( [ _ | Xs ], Y) :- lastelem(Xs, Y).

																				lastelemCut( [ X ], X) :- !.
																				lastelemCut( [ _ | Xs ], Y) :- lastelemCut(Xs, Y).

																				initelem( [ _ ], []).
																				initelem( [  X | Xs ], [ X | Ys ]) :- initelem(Xs, Ys).

																				listlength( [], 0).
																				listlength( [ _ | L ], N) :- listlength(L, N0), N is 1+N0.

																				sumList( [], 0).
																				sumList( [ X | Xs], N):- sumList(Xs, N0), N is X+N0.

																				appendList( [], Ys, Ys).
																				appendList( [X | Xs], Ys, [X | Zs]) :- appendList(Xs, Ys, Zs).

																				factorial(0, 1). 
																				factorial(N, Result) :- N > 0, M is N -1, factorial(M, S), Result is N * S.
