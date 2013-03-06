class REPLACE_MODULE_NAME::service {
  service { "REPLACE_MODULE_NAME":
    ensure => running,
    hasstatus => true,
    hasrestart => true,
    enable => true,
    require => Class["REPLACE_MODULE_NAME::config"],
  }
}

