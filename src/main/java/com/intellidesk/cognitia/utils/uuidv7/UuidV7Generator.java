package com.intellidesk.cognitia.utils.uuidv7;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

public class UuidV7Generator implements IdentifierGenerator {
    @Override
    public Object generate(SharedSessionContractImplementor session, Object object) {
        return UuidUtils.randomV7();
    }
}
