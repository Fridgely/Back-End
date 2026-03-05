package soon.fridgely.domain.refrigerator.dto.command;

import soon.fridgely.domain.refrigerator.entity.MemberRefrigerator;

import java.util.List;

public record CachedMemberRefrigerators(
    long memberId,
    List<CachedRefrigeratorInfo> refrigerators
) {

    public static CachedMemberRefrigerators from(long memberId, List<MemberRefrigerator> refrigerators) {
        return new CachedMemberRefrigerators(
            memberId,
            refrigerators.stream()
                .map(CachedRefrigeratorInfo::from)
                .toList()
        );
    }

}