package soon.fridgely;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import soon.fridgely.domain.auth.controller.AuthController;
import soon.fridgely.domain.auth.service.AuthService;
import soon.fridgely.domain.category.controller.CategoryController;
import soon.fridgely.domain.category.service.CategoryService;
import soon.fridgely.domain.food.controller.FoodController;
import soon.fridgely.domain.food.service.FoodService;
import soon.fridgely.domain.member.controller.MemberController;
import soon.fridgely.domain.member.service.MemberService;
import soon.fridgely.global.security.jwt.filter.JwtAuthenticationFilter;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(controllers = {
    MemberController.class,
    AuthController.class,
    CategoryController.class,
    FoodController.class
})
public abstract class ControllerTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockitoBean
    protected JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    protected MemberService memberService;

    @MockitoBean
    protected AuthService authService;

    @MockitoBean
    protected CategoryService categoryService;

    @MockitoBean
    protected FoodService foodService;

}