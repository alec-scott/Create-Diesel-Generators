#!/usr/bin/env perl
# Converts Create Diesel Generators' 1.21.1 NeoForge data (src/main/resources/data, src/generated/resources/data) to
# 26.1 / Create Fly formats. Adapted from the Crafts & Additions / New Age converters (convert_data_26.pl).
# Run once from the repo root, on the unconverted data (not safe to rerun): perl porting-tools/convert_data_cdg_26.pl
#
# - ingredients -> "id" / "#tag"; neoforge:difference -> fabric:difference ("fabric:type").
# - Create processing recipes -> Create Fly codecs ("ingredients":[x] -> "ingredient": x for crushing/cutting; mixing
#   fluid inputs -> fluid_ingredients, fluid outputs -> fluid_results; mB -> droplets x81).
# - CDG's own recipe types:
#     hammering, wire_cutting:   { ingredient, results[] }
#     casting:                   { fluid_ingredient, mold, results[] }
#     distillation:              { fluid_ingredient, fluid_results[], heat_requirement?, processing_time }
#     basin_fermenting, bulk_fermenting: mixing-style { ingredients[], fluid_ingredients[], results[], fluid_results[],
#                                heat_requirement?, processing_time }
#     compression_molding:       { ingredients[], mold, heat_requirement?, results[] }
# - neoforge:conditions -> fabric:load_conditions.
use strict;
use warnings;
use File::Find;
use JSON::PP;

my $json = JSON::PP->new->utf8->canonical->pretty->indent_length(2)->space_before(0);
my @dirs = ('src/main/resources/data/createdieselgenerators/recipe', 'src/generated/resources/data/createdieselgenerators/recipe',
    'src/generated/resources/data/createdieselgenerators/advancement');
my %count;
sub read_json { my $f = shift; open my $in, '<:raw', $f or die "$f: $!"; local $/; my $d = $json->decode(<$in>); close $in; return $d; }
sub write_json { my ($f, $d) = @_; open my $out, '>:raw', $f or die "$f: $!"; print $out $json->encode($d); close $out; }

sub ingredient {
    my $in = shift;
    return $in unless ref $in;
    if (ref $in eq 'ARRAY') { my @a = map { ingredient($_) } @$in; return @a == 1 ? $a[0] : \@a; }
    return $in->{item} if exists $in->{item};
    return "#$in->{tag}" if exists $in->{tag};
    if (($in->{type} // '') eq 'neoforge:difference') {
        return { 'fabric:type' => 'fabric:difference', base => ingredient($in->{base}), subtracted => ingredient($in->{subtracted}) };
    }
    die 'unknown ingredient ' . $json->encode($in);
}
sub is_fluid_ing { my $x = shift; return ref $x eq 'HASH' && ($x->{type} // '') =~ /^(fluid_stack|fluid_tag)$/; }
sub fluid_ingredient {
    my $f = shift;
    my %o = (type => $f->{type}, amount => $f->{amount} * 81);
    if ($f->{type} eq 'fluid_tag') { $o{fluid_tag} = '#' . $f->{fluid_tag}; }
    else { $o{fluid} = $f->{fluid}; $o{components} = $f->{components} if exists $f->{components}; }
    return \%o;
}
sub is_fluid_out { my $r = shift; return ref $r eq 'HASH' && exists $r->{amount}; }
sub fluid_out {
    my $r = shift;
    my %o = (id => $r->{id}, amount => $r->{amount} * 81);
    $o{components} = $r->{components} if exists $r->{components};
    return \%o;
}
sub output {
    my $r = shift;
    my %out = (id => $r->{id});
    $out{count} = $r->{count} if ($r->{count} // 1) != 1;
    $out{chance} = $r->{chance} if exists $r->{chance} && $r->{chance} != 1;
    $out{components} = $r->{components} if exists $r->{components};
    return \%out;
}
sub split_ingredients {
    my ($ing) = @_;
    my @i = grep { !is_fluid_ing($_) } @$ing;
    my @f = grep { is_fluid_ing($_) } @$ing;
    return ([ map { ingredient($_) } @i ], [ map { fluid_ingredient($_) } @f ]);
}
sub split_results {
    my ($res) = @_;
    my @i = grep { !is_fluid_out($_) } @$res;
    my @f = grep { is_fluid_out($_) } @$res;
    return ([ map { output($_) } @i ], [ map { fluid_out($_) } @f ]);
}
sub condition {
    my $c = shift;
    my $t = $c->{type};
    return { condition => 'fabric:all_mods_loaded', values => [ $c->{modid} ] } if $t eq 'neoforge:mod_loaded';
    if ($t eq 'neoforge:not' && $c->{value}{type} eq 'neoforge:tag_empty') {
        (my $tag = $c->{value}{tag}) =~ s/^neoforge:/c:/;
        return { condition => 'fabric:tags_populated', registry => 'minecraft:item', values => [ $tag ] };
    }
    die "unhandled condition $t";
}
sub conditions { my @c = map { condition($_) } @{ shift() }; return @c == 1 ? $c[0] : { condition => 'fabric:and', values => \@c }; }
sub heat { my ($r, $o) = @_; my $h = $r->{heat_requirement} // $r->{heatRequirement}; $o->{heat_requirement} = $h if defined $h; }
sub time_ { my ($r, $o, $def) = @_; my $t = $r->{processing_time} // $r->{processingTime} // $def; $o->{processing_time} = $t if defined $t; }

sub convert_recipe {
    my $r = shift;
    my %out;
    $out{'fabric:load_conditions'} = conditions($r->{'neoforge:conditions'}) if $r->{'neoforge:conditions'};
    my $type = $r->{type};
    $out{type} = $type;
    for my $k (qw(category group show_notification)) { $out{$k} = $r->{$k} if exists $r->{$k}; }
    my @ing = ref $r->{ingredients} eq 'ARRAY' ? @{ $r->{ingredients} } : ();

    if ($type eq 'minecraft:crafting_shaped' || $type eq 'create:mechanical_crafting') {
        $out{pattern} = $r->{pattern};
        $out{key} = { map { $_ => ingredient($r->{key}{$_}) } keys %{ $r->{key} } };
        $out{accept_mirrored} = $r->{accept_mirrored} if exists $r->{accept_mirrored};
        $out{result} = output($r->{result});
    } elsif ($type eq 'minecraft:stonecutting') {
        $out{ingredient} = ingredient($r->{ingredient});
        $out{result} = output($r->{result});
    } elsif ($type eq 'create:mixing' || $type eq 'create:compacting') {
        $out{type} = 'create:mixing'; # Create Fly's compacting has no fluid output; only plant_oil is affected
        my ($i, $f) = split_ingredients(\@ing);
        my ($ri, $rf) = split_results($r->{results});
        $out{ingredients} = $i if @$i;
        $out{fluid_ingredients} = $f if @$f;
        $out{results} = $ri if @$ri;
        $out{fluid_results} = $rf if @$rf;
        heat($r, \%out); time_($r, \%out);
    } elsif ($type eq 'create:crushing' || $type eq 'create:cutting') {
        $out{ingredient} = ingredient($ing[0]);
        $out{processing_time} = $r->{processing_time} // ($type eq 'create:crushing' ? 100 : 50);
        $out{results} = [ map { output($_) } @{ $r->{results} } ];
    } elsif ($type eq 'create:deploying') {
        $out{target} = ingredient($ing[0]);
        $out{ingredient} = ingredient($ing[1]);
        $out{results} = [ map { output($_) } @{ $r->{results} } ];
        $out{keep_held_item} = JSON::PP::true if $r->{keep_held_item};
    } elsif ($type eq 'createdieselgenerators:hammering' || $type eq 'createdieselgenerators:wire_cutting') {
        $out{ingredient} = ingredient($ing[0]);
        $out{results} = [ map { output($_) } @{ $r->{results} } ];
    } elsif ($type eq 'createdieselgenerators:casting') {
        $out{fluid_ingredient} = fluid_ingredient($ing[0]);
        $out{mold} = $r->{mold};
        $out{results} = [ map { output($_) } @{ $r->{results} } ];
    } elsif ($type eq 'createdieselgenerators:distillation') {
        $out{fluid_ingredient} = fluid_ingredient($ing[0]);
        my ($ri, $rf) = split_results($r->{results});
        die 'distillation item result' if @$ri;
        $out{fluid_results} = $rf;
        heat($r, \%out); time_($r, \%out);
    } elsif ($type eq 'createdieselgenerators:basin_fermenting' || $type eq 'createdieselgenerators:bulk_fermenting') {
        my ($i, $f) = split_ingredients(\@ing);
        my ($ri, $rf) = split_results($r->{results});
        $out{ingredients} = $i if @$i;
        $out{fluid_ingredients} = $f if @$f;
        $out{results} = $ri if @$ri;
        $out{fluid_results} = $rf if @$rf;
        heat($r, \%out); time_($r, \%out);
    } elsif ($type eq 'createdieselgenerators:compression_molding') {
        $out{ingredients} = [ map { ingredient($_) } @ing ];
        $out{mold} = $r->{mold};
        heat($r, \%out);
        $out{results} = [ map { output($_) } @{ $r->{results} } ];
    } else {
        die "unsupported recipe type $type";
    }
    return \%out;
}

sub convert_advancement {
    my $a = shift;
    my %out = %$a;
    delete $out{'neoforge:conditions'};
    $out{'fabric:load_conditions'} = conditions($a->{'neoforge:conditions'}) if $a->{'neoforge:conditions'};
    return \%out;
}

for my $dir (grep { -d } @dirs) {
    my @files;
    find(sub { push @files, $File::Find::name if /\.json$/ }, $dir);
    for my $f (sort @files) {
        if ($f =~ m{/compat/(electroenergetics|immersiveengineering)/}) { unlink $f; $count{'removed (no 26.1 Fabric build)'}++; next; }
        my $d = read_json($f);
        if ($dir =~ /advancement/) { write_json($f, convert_advancement($d)); $count{advancements}++; }
        else { write_json($f, convert_recipe($d)); $count{recipes}++; }
    }
}
print join(', ', map { "$count{$_} $_" } sort keys %count) . "\n";
