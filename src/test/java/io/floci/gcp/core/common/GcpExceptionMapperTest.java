package io.floci.gcp.core.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GcpExceptionMapperTest {

    @Test
    void errorDetailCarriesAllFields() {
        var detail = new GcpExceptionMapper.ErrorDetail(404, "not found", "NOT_FOUND");
        assertEquals(404, detail.code());
        assertEquals("not found", detail.message());
        assertEquals("NOT_FOUND", detail.status());
    }

    @Test
    void errorWrapperWrapsDetail() {
        var detail = new GcpExceptionMapper.ErrorDetail(409, "exists", "ALREADY_EXISTS");
        var wrapper = new GcpExceptionMapper.ErrorWrapper(detail);
        assertSame(detail, wrapper.error());
    }

    @Test
    void mapperProducesCorrectDetailForNotFound() {
        GcpException ex = GcpException.notFound("bucket missing");
        var detail = new GcpExceptionMapper.ErrorDetail(ex.getHttpStatus(), ex.getMessage(), ex.getGcpStatus());
        assertEquals(404, detail.code());
        assertEquals("bucket missing", detail.message());
        assertEquals("NOT_FOUND", detail.status());
    }

    @Test
    void mapperProducesCorrectDetailForAlreadyExists() {
        GcpException ex = GcpException.alreadyExists("bucket exists");
        var detail = new GcpExceptionMapper.ErrorDetail(ex.getHttpStatus(), ex.getMessage(), ex.getGcpStatus());
        assertEquals(409, detail.code());
        assertEquals("ALREADY_EXISTS", detail.status());
    }
}
