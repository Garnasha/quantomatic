package quanto.rewrite
import quanto.data._

object Matcher {
  def findMatches(pat: Graph, tgt: Graph, restrictTo: Set[VName]): Stream[Match] = {
    val patVars = Vector()
    val tgtVars = Vector()
    val patNodes = pat.verts.filter(!pat.vdata(_).isWireVertex)
    val patWires = pat.verts.filter(pat.vdata(_).isWireVertex)

    // TODO: how is this set used? (tVerts?)
//    val concrete = pat.verts.filter{ v =>
//      !pat.vdata(v).isWireVertex && pat.bboxesContaining(v).isEmpty
//    }

    val ms = MatchState(
      m = Match(pat, tgt),
      finished=false,
      uNodes = patNodes,
      uWires = patWires,
      pNodes = Set.empty,
      psNodes = Set.empty,
      tVerts = restrictTo,
      angleMatcher = AngleExpressionMatcher(patVars,tgtVars))

    // TODO: !-boxes
    ms.matchPending().flatMap { ms1 =>
      if (ms1.finished) Stream(ms1.m) else Stream()
    }
  }

  def findMatches(pat: Graph, tgt: Graph): Stream[Match] =
    findMatches(pat, tgt, tgt.verts)

}