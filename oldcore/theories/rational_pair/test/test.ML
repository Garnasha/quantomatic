local
  open RP_Theory
  fun assert_g_eq msg exp actual =
    if Graph.exact_eq exp actual then ()
    else (writeln "Expected:";
          Graph.print exp;
          writeln "Actual:";
          Graph.print actual;
          raise ERROR (msg^": graphs differed"))
in
  val _ = Testing.test (theory_name^" theory save/restore") (fn () => let
    val g = Graph.empty
        |> Graph.add_named_vertex (V.mk "a")
              (Graph.NVert (RP_Data.Bnd (LinratExpr.zero)))
        |> Graph.add_named_vertex (V.mk "b")
              (Graph.NVert (RP_Data.Bnd (LinratExpr.parse "a + b")))
        |> Graph.add_named_vertex (V.mk "c")
              (Graph.NVert (RP_Data.Wnd (LinratExpr.zero)))
        |> Graph.add_named_vertex (V.mk "d")
              (Graph.NVert (RP_Data.Wnd (LinratExpr.parse "c")))
        |> Graph.add_named_vertex (V.mk "w") Graph.WVert
        |> Graph.add_named_edge (E.mk "e1") (Directed,()) (V.mk "a") (V.mk "w")
        |> Graph.add_named_edge (E.mk "e2") (Directed,()) (V.mk "w") (V.mk "b")
    val g' = GraphJSON.input (GraphJSON.output g)
    val _ = assert_g_eq "input (output g) = g" g g'
    (* if the file format changes, do
     * cp graphs/current_format.graph graphs/v<n>_format.graph
     * and add a new read test (see above) *)
    val _ = File_Io.write_json "graphs/current_format.graph" (GraphJSON.output g)
  in () end) ()
  val _ = Testing.assert_no_failed_tests()
end
