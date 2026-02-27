package com.online.attendance.invoicepdf.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartyInfo {
    private String name;
    private String addressLine1;
    private String addressLine2;
    private String addressLine3;
    private String phone;
    private String email;
    private String vatNumber;
    private String attn;
}
