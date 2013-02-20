class REPLACE_MODULE_NAME::config {
    file { "/opt/REPLACE_MODULE_NAME":
    ensure => directory,
    owner => 'root',
    group => 'root',
    mode => 0755,
}
file { "/opt/REPLACE_MODULE_NAME/start":
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

   file { "/opt/stage/REPLACE_MODULE_NAME":
    ensure => directory,
    owner => 'root',
    group => 'root',
    mode => 0755,
    recurse => true,
    purge => true,
    source => "puppet:///modules/REPLACE_MODULE_NAME/stage/REPLACE_MODULE_NAME",
    notify => Class[REPLACE_MODULE_NAME::service]
}

  file { "/opt/REPLACE_MODULE_NAME/RUNNING_PID":
    owner => 'root',
    group => 'root',
    mode  => 0644,
    require => File["/opt/REPLACE_MODULE_NAME"]
}
file { "/etc/init.d/REPLACE_MODULE_NAME":
    ensure => present,
    owner  => 'root',
    group => 'root',
    mode  => 0755,
    source => "puppet:///REPLACE_MODULE_NAME/bin/REPLACE_MODULE_NAME",
}
}
