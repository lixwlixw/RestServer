package com.asiainfo.ocmanager.dacp;

/**
 * Created by YANLSH
 * Created on 2017/7/3
 */
public enum DriverTypeEnum {
    ORACLE("oracle.jdbc.dirver.OracleDriver","oracle"), MySQL("com.mysql.jdbc.Driver","mysql"),
    DB2("com.ibm.db2.jdbc.net.DB2Driver","db2"), HIVE("org.apache.hadoop.hive.jdbc.HiveDriver","hive"),
    GREENPLUM("com.pivotal.jdbc.GreenplumDriver","greenplum"),MONGODB("mongodb.jdbc.MongoDriver","mongodb"),
    NEO4J("org.neo4j.jdbc.Driver","neo4j"),GBASE("com.gbase.jdbc.Driver","gbase"),
    TERADATA("com.ncr.teradata.TeraDriver","teradata"),SQLSERVER("com.microsoft.sqlserver.jdbc.SQLServerDriver","sqlserver"),
    POSTGRE("org.postgresql.Driver","postgresql");
    /*HDFS("","hdfs"),
    SPARK("","spark"),
    HBASE("","hbase"),
    KAFKA("","kafka"),
    MAPREDUCE("","mapreduce");*/

    String driveClassname;

    String driveType;

    DriverTypeEnum(String driveClassname, String driveType) {
        this.driveClassname = driveClassname;
        this.driveType = driveType;
    }



    public static String getDriverTypeEnum(String driveTypeStr){
        for (DriverTypeEnum c : DriverTypeEnum.values()) {
            if (c.getDriveType().equals(driveTypeStr)) {
                return c.driveClassname;
            }
        }
        return null;
    }

    public String getDriveType() {
        return driveType;
    }
}
