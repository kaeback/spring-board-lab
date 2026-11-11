package net.datasa.board.web;

/**
 * 세션에 저장하는 속성 키 모음.
 * 문자열을 여기저기 하드코딩하지 않도록 한 곳에 모아 둔다.
 */
public abstract class SessionConst {

    /** 로그인한 회원을 세션에 보관할 때 쓰는 키 (Thymeleaf의 ${session.loginMember}와 동일) */
    public static final String LOGIN_MEMBER = "loginMember";
}
