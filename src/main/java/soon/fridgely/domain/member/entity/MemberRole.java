package soon.fridgely.domain.member.entity;

import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.util.Arrays;

public enum MemberRole {
    MEMBER,
    ADMIN;

    public static MemberRole from(String role) {
        return Arrays.stream(MemberRole.values())
            .filter(r -> r.name().equalsIgnoreCase(role))
            .findFirst()
            .orElseThrow(() -> new CoreException(ErrorType.INVALID_REQUEST));
    }

}