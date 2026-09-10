package saludfinanciera.finanzas.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConsultaAiInputDTO {
    private String query;
    private String email;
}
