
				Patching of Torque 3.0.2 for OJB 1.0.x

Version of this file:
CVS $Id: readme_torque_patch.txt,v 1.1 2007-08-24 22:17:36 ewestfal Exp $

§ What has been done?

 Changes:

 1. A new platform "oracle9" have been patched into the Torque 3.0.2 JAR by
  copying the files for the "oracle" platform and in sql/base/oracle9/db.props
  replacing the following line:
   TIMESTAMP = DATE
  with:
   TIMESTAMP = TIMESTAMP

 2. The "sapdb" platform have been patched by changing sql/base/sapdb/db.props
  and replacing the following line:
   CLOB = LONG UNICODE
  with:
   CLOB = LONG VARCHAR

 3. The "hypersonic" platform have been patched by changing
  sql/base/hypersonic/db.props and replacing the following lines:
   BLOB =
   CLOB =
  with:
   BLOB = LONGVARBINARY
   CLOB = VARCHAR

 4. The "postgresql" platform have been patched by changing
  sql/base/postgresql/db.props and replacing the following lines:
   DOUBLE = double
   BLOB =
  with:
   DOUBLE = float8
   BLOB = bytea



§ Why has this been done?

  In order to make the regression test for OJB 1.0-branch perform better on
 Oracle 9 or higher and to excersise the correct datatypes while doing BLOB
 and CLOB tests.

  We have decided to patch the JAR rather than submitting changes to the
 Torque project, since Torque 3.0.2 is and old and deprecated version and
 the OJB project for the OJBv1.1 CVS-trunk have already moved on to current
 Torque release (v3.1.1 at the time of writing).


§ Patch results for OJB oracle9i.profile:

$ diff old/sql/ojbcore-schema.sql new/sql/
31c31
<     TIMESTAMP_ DATE,
---
>     TIMESTAMP_ TIMESTAMP,

$ diff old/sql/ojbtest-schema.sql new/sql/
313c313
<     TIMESTAMP_ DATE
---
>     TIMESTAMP_ TIMESTAMP
495c495
<     relatedValue3 DATE
---
>     relatedValue3 TIMESTAMP
516c516
<     contract_value4 DATE
---
>     contract_value4 TIMESTAMP
536c536
<     version_value3 DATE
---
>     version_value3 TIMESTAMP
556c556
<     eff_value3 DATE
---
>     eff_value3 TIMESTAMP
575c575
<     value3 DATE,
---
>     value3 TIMESTAMP,
578c578
<     value6 DATE,
---
>     value6 TIMESTAMP,
2736c2736
<     P_DATE DATE,
---
>     P_DATE TIMESTAMP,
