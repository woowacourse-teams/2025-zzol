package coffeeshout.cardgame.domain.card;

public class AdditionCard extends Card {

    public static final AdditionCard PLUS_40 = new AdditionCard(40);
    public static final AdditionCard PLUS_35 = new AdditionCard(35);
    public static final AdditionCard PLUS_30 = new AdditionCard(30);
    public static final AdditionCard PLUS_25 = new AdditionCard(25);
    public static final AdditionCard PLUS_20 = new AdditionCard(20);
    public static final AdditionCard PLUS_15 = new AdditionCard(15);
    public static final AdditionCard PLUS_10 = new AdditionCard(10);
    public static final AdditionCard PLUS_5 = new AdditionCard(5);
    public static final AdditionCard ZERO = new AdditionCard(0);
    public static final AdditionCard MINUS_5 = new AdditionCard(-5);
    public static final AdditionCard MINUS_10 = new AdditionCard(-10);
    public static final AdditionCard MINUS_15 = new AdditionCard(-15);
    public static final AdditionCard MINUS_20 = new AdditionCard(-20);
    public static final AdditionCard MINUS_25 = new AdditionCard(-25);
    public static final AdditionCard MINUS_30 = new AdditionCard(-30);
    public static final AdditionCard MINUS_35 = new AdditionCard(-35);
    public static final AdditionCard MINUS_40 = new AdditionCard(-40);

    public AdditionCard(int value) {
        super(CardType.ADDITION, value);
    }
}
