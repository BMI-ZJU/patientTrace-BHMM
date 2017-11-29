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
    <presc name="" dosage="" unit="" quantity="" frequency="" date=""/>
  </presces>
  <operations>
    <operation startTime="" stopTime="">
      <item name=""></item>
    </operation>
  </operations>
</patientTrace>
```

特点：

* 无时序信息，以日期记录时间
* 所有的医嘱用药都未分散到每一天，（用药只包含住院用药，出院带药被去除）
* 一个病人存为一个文件
* 需后续进一步处理，同时作为处理后数据的一个参考

### 进一步处理

先用csv存储吧

```
[patientId], 第1天, 第2天, ……, 第n天
静脉抽血,1,0,...,1
血常规+CRP,0,1,...,0
...
牛黄解毒片,0,0,...,9
...
冠状动脉造影术,0,1...,0
```

特点：

* 无日期，所有时间记录为第n天
* 处方分散到每一天。简单的计算方式，每天用量为`dosage *  frequency` ，持续时间为`quantity / (dosage * freq)`，超出住院时间的舍去。
* 一些无单位的，或者医嘱，手术信息用`1`表示。
* 因为相同用药单位相同，在csv中单位不做记录

## 几个问题

1. 对于处方的数据库，在`PRESC_MASTER`数据表中，以`PRESC_NO`和`PRESC_DATE`作为主键，在`PRESC_DETAIL`中，同样以`PRESC_NO`,`PRESC_DATE`和`ITEM_NO`作为主键。但是仅`PRESC_NO`	作为外键，数据库结构存在问题。
2. 几个病人的数据质量比较差，时间信息上存在问题。如`581855_2.xml`，手术时间比出院时间晚；如`F341248_1.xml`的手术时间比入院时间早；如`C547455_1.xml`的处方信息起始时间比出院时间晚。
3. 处方剂量上存在问题。如`448_1.xml`，处方信息中有一个为**聚乙二醇电解质散**，一项纪录为`<presc name="聚乙二醇电解质散" dosage="246.6" unit="g" quantity="18" frequency="1/日" date="" />`，另一项纪录为`<presc name="聚乙二醇电解质散" dosage="1" unit="袋" quantity="18" frequency="3/日" date="" />`，两者之间单位不同。人为理解大概为一袋是246.6g，quantity指的也是18袋。但数据转换程序中未作处理。
4. 还有无法简单处理的。仍旧是`448_1.xml`中，滴鼻液和滴眼液，应该是一瓶一瓶的，dosage=0.1，unit=支，quantity=1。未知dosage以什么为单位，猜想应该是`ml`，但一瓶多少`ml`是未知的。不能得出持续时间为`quantity / (dosage * freq)`

