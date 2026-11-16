package mx.trazable.gps.core.config;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@ControllerAdvice
public class WebServiceResponse {


    @ExceptionHandler(MissingRequestHeaderException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public WebServiceResponse handleMissingRequestHeaderException(MissingRequestHeaderException e) {
        return new WebServiceResponse(false, "El header '" + e.getHeaderName() + "' es necesario.");
    }


    private boolean success;

    private String message;

    private Object data;
    private Integer count;

    private WebServiceResponse() {
    }

    public WebServiceResponse(boolean success, String message, List<?> list) {
        this.success = success;
        this.message = message;
        this.data = list;
    }

    public WebServiceResponse(boolean success, String message, Map<String, Object> data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public WebServiceResponse(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public WebServiceResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public WebServiceResponse(boolean success) {
        this.success = success;
        this.message = success ? "OK" : "Error";
    }

    public WebServiceResponse(boolean success, String message, String key, Object value) {
        this.success = success;
        this.message = message;
        this.data = Collections.singletonMap(key, value);
    }

    public WebServiceResponse(Object data) {
        this.success = true;
        this.message = "OK";
        this.data = data;
    }

    public Object getData() {
        if (this.data == null) {
            this.data = new HashMap();
        }

        return this.data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public WebServiceResponse(Object data, Integer count) {
        this.success = true;
        this.message = "OK";
        this.data = data;
        this.count = count;
    }

    public boolean isSuccess() {
        return this.success;
    }

    public String getMessage() {
        return this.message;
    }

    public Integer getCount() {
        return this.count;
    }

    public void setSuccess(final boolean success) {
        this.success = success;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public void setCount(final Integer count) {
        this.count = count;
    }
}

