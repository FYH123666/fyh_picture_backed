package com.fyh.model.dto.user;

import lombok.Data;

@Data
public class DeleteRequest {
    private Long id;

    private Long[] ids;
}
