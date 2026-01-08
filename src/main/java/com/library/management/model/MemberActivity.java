package com.library.management.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberActivity {
    private Long memberId;
    private String memberName;
    private String memberEmail;
    private Long loanCount;
}
