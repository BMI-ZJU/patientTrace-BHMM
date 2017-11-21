# Topic Model

BHMM for treatment event and inpatient journey analysis





## 数据格式

### 从数据库中收集信息

```xml
<patientTrace patientId="" admissionTime="" dischargeTime="">
  <orders>
    <order name="" dosage="" unit="" startTime="" stopTime=""/>
  </orders>
  <presces>
    <presc name="" dosage="" unit="" number="" frequency="" />
  </presces>
  <operations>
    <operation name="" startTime="" stopTime=""></operation>
  </operations>
</patientTrace>
```

特点：

* 无时序信息，以日期记录时间
* 需后续进一步处理

### 使用

