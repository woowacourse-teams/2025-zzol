package coffeeshout.numberpoker.domain;

public class HandRanking implements Comparable<HandRanking> {

    private enum Type {
        PAIR, HIGH_CARD
    }

    private final Type type;
    private final int primary;    // PAIR: 페어 숫자 / HIGH_CARD: 높은 숫자
    private final int secondary;  // PAIR: 0 (미사용) / HIGH_CARD: 낮은 숫자

    private HandRanking(Type type, int primary, int secondary) {
        this.type = type;
        this.primary = primary;
        this.secondary = secondary;
    }

    public static HandRanking of(PokerCard card1, PokerCard card2) {
        if (card1.value() == card2.value()) {
            return new HandRanking(Type.PAIR, card1.value(), 0);
        }
        final int high = Math.max(card1.value(), card2.value());
        final int low = Math.min(card1.value(), card2.value());
        return new HandRanking(Type.HIGH_CARD, high, low);
    }

    public boolean isPair() {
        return type == Type.PAIR;
    }

    @Override
    public int compareTo(HandRanking other) {
        if (this.type == Type.PAIR && other.type == Type.HIGH_CARD) {
            return 1;
        }
        if (this.type == Type.HIGH_CARD && other.type == Type.PAIR) {
            return -1;
        }
        if (this.primary != other.primary) {
            return Integer.compare(this.primary, other.primary);
        }
        return Integer.compare(this.secondary, other.secondary);
    }
}
