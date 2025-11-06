package soon.fridgely.domain.refrigerator.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;

@RequiredArgsConstructor
@Component
public class RefrigeratorManager {

    private final RefrigeratorRepository refrigeratorRepository;

    public Refrigerator register(Member member) {
        Refrigerator register = Refrigerator.register(member.getNickname());
        return refrigeratorRepository.save(register);
    }

}