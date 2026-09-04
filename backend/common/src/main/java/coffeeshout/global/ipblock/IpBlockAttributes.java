package coffeeshout.global.ipblock;

public final class IpBlockAttributes {

    /**
     * 매핑된 핸들러가 없어 404가 난 요청(NoResourceFoundException)에 RestExceptionHandler가 설정하는 request attribute.
     * IpBlockFilter는 이 속성이 있을 때만 404 카운터를 올린다. 컨트롤러가 낸 404
     * (ResponseEntity.notFound(), ResponseStatusException, BusinessException)는 속성이 없어 세지 않는다(#1757).
     */
    public static final String UNMATCHED_NOT_FOUND = "ip.block.count.not.found";

    private IpBlockAttributes() {}
}
