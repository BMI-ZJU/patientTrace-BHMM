# Topic Model

![](https://img.shields.io/badge/java-v1.8-brightgreen.svg) ![](https://img.shields.io/badge/compile-failure-red.svg)

BHMM for treatment event and inpatient journey analysis

## 数据格式

### 从数据库中收集信息

```xml
<patientTrace patientId="" admissionTime="" dischargeTime="">
  <orders>
    <order name="" dosage="" unit="" startTime="" stopTime=""/>
  </orders>
  <presces>
    <presc name="" dosage="" unit="" number="" frequency="" date=""/>
  </presces>
  <operations>
    <operation name="" startTime="" stopTime=""></operation>
  </operations>
</patientTrace>
```

特点：

* 无时序信息，以日期记录时间
* 所有的医嘱用药都未分散到每一天
* 一个病人存为一个文件
* 需后续进一步处理

### 使用

## 几个问题

1. 对于处方的数据库，在`PRESC_MASTER`数据表中，以`PRESC_NO`和`PRESC_DATE`作为主键，在`PRESC_DETAIL`中，同样以`PRESC_NO`,`PRESC_DATE`和`ITEM_NO`作为主键。但是仅`PRESC_NO`	作为外键，数据库结构存在问题。

