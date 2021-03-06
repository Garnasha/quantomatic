(*

(* include quanto heap *)
PolyML.SaveState.loadState "../core/heaps/quanto.polyml-heap";

(* build cosy *)
PolyML.Project.use_root "ROOT.ML";

(* include polychrome API *)
OS.FileSys.chDir "../../PolyChrome/polychrome/poly";
PolyML.Project.basic_use "ROOT.ML";
OS.FileSys.chDir "../../../quantomatic/cosy";

(* output the heap *)
(*PolyML.Project.Log.log 2 "finished updating polysaves";
PolyML.Project.ensure_at_latest_heap ();
PolyML.Project.Log.log 2 "finished ensure_at_latest_heap";*)
(*PolyML.shareCommonData run; PolyML.fullGC ();
PolyML.Project.Log.log 2 "finished garbage collection";*)
PolyML.SaveState.saveState "chromeui/polychrome.polyml-heap";
PolyML.Project.Log.log 2 "finished saving global heap";
val _ = OS.Process.exit OS.Process.success;

*)

(* build cosy *)
val _ = PolyML.exception_trace (fn () => PolyML.use "ROOT.ML")
        handle _ => OS.Process.exit OS.Process.failure;

PolyML.Project.Log.log 2 "finished updating polysaves";

PolyML.Project.ensure_at_latest_heap ();

PolyML.Project.Log.log 2 "finished ensure_at_latest_heap";

(* include polychrome API *)
OS.FileSys.chDir "../../PolyChrome/polychrome/poly";
PolyML.Project.basic_use "ROOT.ML";
OS.FileSys.chDir "../../../quantomatic/cosy";

PolyML.Project.make "chromeui/cosy_util.sml";

PolyML.shareCommonData run; PolyML.fullGC ();

PolyML.Project.Log.log 2 "finished garbage collection";

PolyML.SaveState.saveState "chromeui/polychrome.polyml-heap";

PolyML.Project.Log.log 2 "finished saving global heap";

val _ = OS.Process.exit OS.Process.success;
