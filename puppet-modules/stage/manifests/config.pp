class stage::config {
    file { "/opt/stage":
        ensure => directory,
        owner => 'root',
        group => 'root',
        mode => 0755,
    }
}