package chess
package variant

import chess.format.EpdFen
import bitboard.Bitboard

case object RacingKings
    extends Variant(
      id = Variant.Id(9),
      key = Variant.LilaKey("racingKings"),
      uciKey = Variant.UciKey("racingkings"),
      name = "Racing Kings",
      shortName = "Racing",
      title = "Race your King to the eighth rank to win.",
      standardInitialPosition = false
    ):

  override val allowsCastling = false

  // Both sides start on the first two ranks:
  // krbnNBRK
  // qrbnNBRQ
  override val pieces: Map[Pos, Piece] = Map(
    Pos.A1 -> Black.queen,
    Pos.A2 -> Black.king,
    Pos.B1 -> Black.rook,
    Pos.B2 -> Black.rook,
    Pos.C1 -> Black.bishop,
    Pos.C2 -> Black.bishop,
    Pos.D1 -> Black.knight,
    Pos.D2 -> Black.knight,
    Pos.E1 -> White.knight,
    Pos.E2 -> White.knight,
    Pos.F1 -> White.bishop,
    Pos.F2 -> White.bishop,
    Pos.G1 -> White.rook,
    Pos.G2 -> White.rook,
    Pos.H1 -> White.queen,
    Pos.H2 -> White.king
  )

  override val castles = Castles.none

  override val initialFen = EpdFen("8/8/8/8/8/8/krbnNBRK/qrbnNBRQ w - - 0 1")

  def validMoves(situation: Situation): List[Move] =
    import situation.{ genSafeKing, genNonKing, us, board, sliderBlockers }
    val targets = ~us
    val moves   = genNonKing(targets) ++ genSafeKing(targets)
    moves.filter(kingSafety)

  override def isInsufficientMaterial(board: Board)                  = false
  override def opponentHasInsufficientMaterial(situation: Situation) = false

  private def reachedGoal(board: Board, color: Color) =
    board.kingOf(color).intersects(Bitboard.rank(Rank.Eighth))

  private def reachesGoal(move: Move) =
    reachedGoal(move.situationAfter.board, move.piece.color)

  // It is a win, when exactly one king made it to the goal. When white reaches
  // the goal and black can make it on the next ply, he is given a chance to
  // draw, to compensate for the first-move advantage. The draw is not called
  // automatically, because black should also be given equal chances to flag.
  override def specialEnd(situation: Situation) =
    situation.color match
      case White =>
        reachedGoal(situation.board, White) ^ reachedGoal(situation.board, Black)
      case Black =>
        reachedGoal(situation.board, White) && situation.legalMoves.filter(reachesGoal).isEmpty

  // If white reaches the goal and black also reaches the goal directly after,
  // then it is a draw.
  override def specialDraw(situation: Situation) =
    situation.color.white && reachedGoal(situation.board, White) && reachedGoal(situation.board, Black)

  override def winner(situation: Situation): Option[Color] =
    specialEnd(situation) option Color.fromWhite(reachedGoal(situation.board, White))

  // Not only check that our king is safe,
  // but also check the opponent's
  override def kingSafety(m: Move): Boolean =
    super.kingSafety(m) && m.after.isCheck(!m.color).no

  // When considering stalemate, take into account that checks are not allowed.
  override def staleMate(situation: Situation): Boolean =
    situation.check.no && !specialEnd(situation) && situation.legalMoves.isEmpty
