package coffeeshout.global.filter;

public final class IpBlockAttributes {

    /**
     * BusinessException(404) 처리 시 RestExceptionHandler가 설정하는 request attribute.
     * IpBlockFilter는 이 속성이 있으면 404 카운터 집계를 건너뛴다.
     */
    public static final String BUSINESS_NOT_FOUND = "ip.block.skip.not.found";

    private IpBlockAttributes() {
    }
}
