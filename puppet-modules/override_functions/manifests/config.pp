class override_functions::config {
    file { "/etc/init.d/override_functions":
        ensure => present,
        owner  => 'root',
        group => 'root',
        mode  => 0755,
        source => "puppet:///override_functions/bin/override_functions",
    }
}   