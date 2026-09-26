package coffeeshout.websocket.docs.collision;

/** {@code WsCatalogBuilderTest.FixturePayload} 와 simpleName 이 같아 스키마 이름 충돌을 재현한다. */
public record FixturePayload(int other) {}
