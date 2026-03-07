package com.finventory.dto;

import com.finventory.model.Party.PartyType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PartyDto {
  private UUID id;

  @NotBlank(message = "Party name is required")
  private String name;

  @NotNull(message = "Party type is required")
  private PartyType type;

  private String gstin;

  private String address;

  private String phone;

  @Email(message = "Invalid email format")
  private String email;
}
