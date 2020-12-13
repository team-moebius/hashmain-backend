package com.moebius.backend.domain.apikeys;

import com.moebius.backend.domain.commons.Base;
import com.moebius.backend.domain.commons.Exchange;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@ToString(exclude = {"id", "memberId"})
@Document(collection = "apikeys")
@CompoundIndex(def = "{'memberId': 1, 'exchange': 1}", unique = true)
public class ApiKey extends Base {
    @Id
    private ObjectId id;
    private ObjectId memberId;
    private Exchange exchange;
    private String name;
    private String accessKey;
    private String secretKey;
}
