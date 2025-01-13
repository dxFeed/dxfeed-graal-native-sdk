package com.dxfeed.ipf;

import java.lang.reflect.Field;
import java.util.Collection;

public class InstrumentProfileCustomFields {

  private static final Field CUSTOM_FIELDS_FIELD_INFO;

  static {
    try {
      CUSTOM_FIELDS_FIELD_INFO = InstrumentProfile.class.getDeclaredField("customFields");
      CUSTOM_FIELDS_FIELD_INFO.setAccessible(true);
    } catch (final NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
  }

  private String[] customFields;

  InstrumentProfileCustomFields(InstrumentProfile profile) {
    try {
      customFields = (String[]) CUSTOM_FIELDS_FIELD_INFO.get(profile);
    } catch (final IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  InstrumentProfileCustomFields(String[] customFields) {
    this.customFields = customFields;
  }

  void assignTo(InstrumentProfile profile) {
    try {
      CUSTOM_FIELDS_FIELD_INFO.set(profile, customFields);
    } catch (final IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns custom field value with a specified name.
   *
   * @param name name of custom field.
   * @return custom field value with a specified name.
   */
  private String getField(String name) {
    String[] customFields = this.customFields;

    return customFields == null ? null : ArrayMap.get(customFields, name);
  }

  /**
   * Changes custom field value with a specified name.
   *
   * @param name  name of custom field.
   * @param value custom field value.
   */
  private void setField(String name, String value) {
    String[] customFields = this.customFields;

    if (!value.isEmpty()) {
      this.customFields = ArrayMap.put(customFields == null ? new String[4] : customFields, name,
          value);
    } else if (customFields != null) {
      this.customFields = ArrayMap.putIfKeyPresent(customFields, name, value);
    }
  }

  /**
   * Returns numeric field value with a specified name.
   *
   * @param name name of field.
   * @return field value.
   */
  public double getNumericField(String name) {
    String value = getField(name);

    return value == null || value.isEmpty() ? 0 :
        value.length() == 10 && value.charAt(4) == '-' && value.charAt(7) == '-'
            ? InstrumentProfileField.parseDate(value) :
            InstrumentProfileField.parseNumber(value);
  }

  /**
   * Changes numeric field value with a specified name.
   *
   * @param name  name of field.
   * @param value field value.
   */
  public void setNumericField(String name, double value) {
    setField(name, InstrumentProfileField.formatNumber(value));
  }

  /**
   * Returns day id value for a date field with a specified name.
   *
   * @param name name of field.
   * @return day id value.
   */
  public int getDateField(String name) {
    String value = getField(name);

    return value == null || value.isEmpty() ? 0 : InstrumentProfileField.parseDate(value);
  }

  /**
   * Changes day id value for a date field with a specified name.
   *
   * @param name  name of field.
   * @param value day id value.
   */
  public void setDateField(String name, int value) {
    setField(name, InstrumentProfileField.formatDate(value));
  }

  /**
   * Adds names of non-empty custom fields to the specified collection.
   *
   * @return {@code true} if {@code targetFieldNames} changed as a result of the call
   */
  public boolean addNonEmptyFieldNames(Collection<? super String> targetFieldNames) {
    boolean updated = false;
    String[] customFields = this.customFields;

    if (customFields != null) {
      for (int i = customFields.length & ~1; (i -= 2) >= 0; ) {
        String name = customFields[i];
        String value = customFields[i + 1];

        if (name != null && value != null && !value.isEmpty()) {
          if (targetFieldNames.add(name)) {
            updated = true;
          }
        }
      }
    }

    return updated;
  }

  private static int customHashCode(String[] a) {
    if (a == null) {
      return 0;
    }

    int hash = 0;

    for (int i = a.length & ~1; (i -= 2) >= 0; ) {
      String key = a[i];
      String value = a[i + 1];

      if (key != null && value != null && !value.isEmpty()) {
        hash += key.hashCode() ^ value.hashCode();
      }
    }

    return hash;
  }

  private static boolean customEquals(String[] a, String[] b) {
    return customContainsAll(b, a) && customContainsAll(a, b);
  }

  private static boolean customContainsAll(String[] a, String[] b) {
    if (b == null) {
      return true;
    }

    for (int i = b.length & ~1; (i -= 2) >= 0; ) {
      String key = b[i];
      String value = b[i + 1];

      if (key != null && value != null && !value.isEmpty()) {
        if (a == null || !value.equals(ArrayMap.get(a, key))) {
          return false;
        }
      }
    }

    return true;
  }

  @Override
  public int hashCode() {
    return customHashCode(customFields);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if (o instanceof final InstrumentProfileCustomFields that) {
      return customEquals(customFields, that.customFields);
    }

    return false;
  }
}
