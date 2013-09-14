%%% hw2.mp -- do not edit, generated automatically by hw2.tex
verbatimtex
\documentclass [12pt]{article}
\begin {document}
etex;
\maketitle

\textbf{2.4.3}

a) $Answer(class,contry) := \pi_{class,country}(\sigma_{bore\geq 16}(Classes))$

Answer of the query:

\begin{tabular}{l|l}
class & country \\
\hline \hline
Iowa & 16 \\
North Carolina & 16 \\
Yamoto & 18
\end{tabular}\\

f) $Ships'(ship, class, launched) := Ships\\
Answer(ship) := \pi_{ship}(Outcomes) \cup \pi_{ship}(Ships')$

Answer of the query:

\begin{tabular}{l}
ship \\
\hline \hline
Arizona\\
Bismarck\\
California\\
Duke of York\\
Fuso\\
Hood\\
King George V\\
Kirishima\\
Prince of Wales\\
Rodney\\
Scharnhorst\\
South Dakota\\
Tennessee\\
Washington\\
West Virginia\\
Yamashiro\\
Haruna\\
Hiei\\
Iowa\\
Kongo\\
Missouri\\
Musashi\\
New Jersey\\
North Carolina\\
Ramillies\\
Renown\\
Repulse\\
Resolution\\
Revenge\\
Royal Oak\\
Royal Sovereign\\
Wisconsin
\end{tabular}\\

i) $Outcomes'(ship, battle, result, date) := Outcomes \Join \rho_{Battles(battle,date)}(Battles)\\
Outcomes''(ship, battle', result', date') := Outcomes'\\
Answer(ship) := \pi_{ship}\sigma_{result='damaged' \and date < date'}(Outcomes' \Join Outcomes'')$

Answer of the query:

(No result)

\textbf{2.4.7}

a) $[max(m,n), m+n]$

b) $[0, min(m,n)]$

c) $[0, m \times n]$

d) $[0, max(0, m-n)]$

\textbf{2.5.1}

a) $\sigma_{speed<2.00 \wedge price>500}(PC)=\emptyset $

e) $List(PcRam, PcPrice, LaptopRam, LaptopPrice) := \pi_{ram,price}(PC) \times \pi_{ram,price}(Laptop)\\
\sigma_{PcRam>LaptopRam \wedge PcPrice \leq LaptopPrice} = \emptyset$

\textbf{UML Diagram}

\begin{figure}
\centering
\begin{emp}[classdiag](20, 20)
Class.A("A")()();
Class.B("B")()();
B.e = A.w + (-20, 0);
drawObjects(A, B);
link(inheritance)(B.e -- A.w);
\end{emp}
\caption{A class diagram}
\end{figure}
verbatimtex
\end{document}
etex;
%%% the end.
end.
endinput;
