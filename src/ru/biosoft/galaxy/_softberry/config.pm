package config;

require Exporter;
@ISA = qw(Exporter);
@EXPORT = qw(config);

my %conf = (
    'tmp_dir' => '/tmp',
);

sub config($)
{
    return $conf{shift};
}

1;