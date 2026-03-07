package com.finventory.dto;

import com.finventory.model.Party.PartyType;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PartyDto {
  private UUID id;
  private String name;
  private PartyType type;
  private String gstin;
  private String address;
  private String phone;
  private String email;
}
