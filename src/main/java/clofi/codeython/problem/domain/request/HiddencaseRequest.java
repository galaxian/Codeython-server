package clofi.codeython.problem.domain.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class HiddencaseRequest {

    @NotBlank(message = "입력값은 공백일 수 없습니다.")
    private List<String> inputCase;

    @NotBlank(message = "출력값은 공백일 수 없습니다.")
    private String outputCase;

}