package com.example.admin.livesapplication;

public class Result {

    /**
     * type : 1003
     * operator : request
     * data : {"result":"ok"}
     */

    private int type;
    private String operator;
    private DataBean data;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public DataBean getData() {
        return data;
    }

    public void setData(DataBean data) {
        this.data = data;
    }

    public static class DataBean {
        /**
         * result : ok
         */

        private int result;

        public int getResult() {
            return result;
        }

        public void setResult(int result) {
            this.result = result;
        }

        @Override
        public String toString() {
            return "DataBean{" +
                    "result='" + result + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "Result{" +
                "type=" + type +
                ", operator='" + operator + '\'' +
                ", data=" + data +
                '}';
    }
}
