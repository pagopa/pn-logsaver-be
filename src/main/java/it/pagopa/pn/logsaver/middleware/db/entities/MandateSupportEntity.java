package it.pagopa.pn.logsaver.middleware.db.entities;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import static it.pagopa.pn.logsaver.middleware.db.BaseDao.GSI_INDEX_DELEGATE_STATE;

import java.util.Set;

import it.pagopa.pn.logsaver.utils.DateUtils;

/**
 * Entity di supporto alla delega
 */
@DynamoDbBean
@Data
@NoArgsConstructor
public class MandateSupportEntity {

    public static final String MANDATE_TRIGGERHELPER_PREFIX = "MANDATETRIGGERHELPER#";


    public static final String COL_PK = "pk";
    public static final String COL_SK = "sk";
    public static final String COL_I_TTL = "i_ttl";

    public MandateSupportEntity(MandateEntity source)
    {
        this.setDelegator(source.getDelegator());
        this.setMandateId(source.getMandateId());
        if (source.getValidto() != null)
            this.setTtl(source.getValidto().getEpochSecond());
    }


    public String getMandateId(){
        return this.sk.replace(MANDATE_TRIGGERHELPER_PREFIX, "");
    }
    public void setMandateId(String id){
        this.sk = MANDATE_TRIGGERHELPER_PREFIX + id;
    }

    @Getter(onMethod=@__({@DynamoDbPartitionKey, @DynamoDbAttribute(COL_PK)})) private String delegator;
    @Getter(onMethod=@__({@DynamoDbSortKey, @DynamoDbAttribute(COL_SK)}))  private String sk;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_I_TTL)}))  private Long ttl;
    
}
