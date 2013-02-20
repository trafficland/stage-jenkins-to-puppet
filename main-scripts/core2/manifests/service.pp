class core2::service {
  service { "core2":
    ensure => running,
    hasstatus => true,
    hasrestart => true,
    enable => true,
    require => Class["core2::config"],
  }
}

