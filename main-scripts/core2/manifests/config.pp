class core2::config {
    file { "/opt/core2":
    ensure => directory,
    owner => 'root',
    group => 'root',
    mode => 0755,
}
file { "/opt/core2/start":
    ensure => file,
    owner => 'root',
    group => 'root',
    mode => 0755,
}
    file { "/opt/stage":
    ensure => directory,
    owner => 'root',
    group => 'root',
    mode => 0755,
}

   file { "/opt/stage/core2":
    ensure => directory,
    owner => 'root',
    group => 'root',
    mode => 0755,
    recurse => true,
    purge => true,
    source => "puppet:///modules/core2/stage/core2",
    notify => Class[core2::service]
}

  file { "/opt/core2/RUNNING_PID":
    owner => 'root',
    group => 'root',
    mode  => 0644,
    require => File["/opt/core2"]
}
file { "/etc/init.d/core2":
    ensure => present,
    owner  => 'root',
    group => 'root',
    mode  => 0755,
    source => "puppet:///core2/bin/core2",
}
}
